errors = []
printing_empty = False

#HUNDREDKS = 3


#range(1, 10) provides 1,2,3,4,5,6,7,8,9
#for hundredks in range(1, 10):
for millions in [1,2,3,4,5]:
    training = True
    skipped = 0
    processed = 0
    sentences = 0
    train_count = 0
    test_count = 0
    counter = 0
    print("starting to process "+str(millions)+"M")
    with open("errors.txt", "w", encoding="utf8") as err_f:
        with open("transformed_train_"+str(millions)+"M.conllx", "w", encoding="utf8") as train_f:
            with open("transformed_test_"+str(millions)+"M.conllx", "w", encoding="utf8") as test_f:
                with open("ftb3.1.conllx", encoding="utf8") as in_f:
                    for line in in_f:
                        if line.startswith("<"):
                            skipped += 1
                            if not printing_empty:
                                if training:
                                    train_f.write("\n")
                                    train_count += 1
                                else:
                                    test_f.write("\n")
                                    test_count += 1

                                printing_empty = True
                                sentences += 1
                        else:
                            printing_empty = False
                            processed += 1
                            try:
                                items = line.split("\t")
                                line = items[1]+" "+items[4]
                                if training:
                                    train_f.write(line)
                                    train_f.write("\n")
                                else:
                                    test_f.write(line)
                                    test_f.write("\n")
                            except:
                                err_f.write(str(counter)+"::"+line)
                                errors.append(str(counter)+"::"+line)
                        counter += 1
#                        if training and sentences > hundredks*100000:
                        if training and sentences > millions*1000000:
                            training = False
#    print(str(hundredks)+"x100k done.")
    print(str(millions)+"M done.")
    print("words:"+str(processed))
    print("sentences:"+str(sentences))
    print("train set:"+str(train_count))
    print("test set:"+str(test_count))
#print(errors)

#total in ftb3.1.conllx is about 76369439 words/symbols and about 4366956 sentences
#with a limit of 2 million sentences, we get about 34213820 words and 2000001 sentences
